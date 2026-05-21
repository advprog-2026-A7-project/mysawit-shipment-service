package com.mysawit.shipment.profiling;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.imageio.ImageIO;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class ShipmentFeatureProfileReportWriter {

    private static final String REPORT_BASENAME = "shipment-feature-profiling-report";
    private static final int LEFT_MARGIN = 48;
    private static final int REPORT_WIDTH = 1600;
    private static final int ROW_HEIGHT = 38;

    private final Path reportDirectory;

    ShipmentFeatureProfileReportWriter(Path reportDirectory) {
        this.reportDirectory = reportDirectory;
    }

    Path write(List<ShipmentFeatureProfileResult> results) {
        try {
            Files.createDirectories(reportDirectory);
            String generatedAt = OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            Files.writeString(markdownReportPath(), markdown(generatedAt, results), StandardCharsets.UTF_8);
            Files.writeString(htmlReportPath(), html(generatedAt, results), StandardCharsets.UTF_8);
            writePng(generatedAt, results);
            return markdownReportPath();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    Path markdownReportPath() {
        return reportDirectory.resolve(REPORT_BASENAME + ".md");
    }

    Path htmlReportPath() {
        return reportDirectory.resolve(REPORT_BASENAME + ".html");
    }

    Path pngReportPath() {
        return reportDirectory.resolve(REPORT_BASENAME + ".png");
    }

    private String markdown(String generatedAt, List<ShipmentFeatureProfileResult> results) {
        long passed = results.stream().filter(ShipmentFeatureProfileResult::success).count();
        StringBuilder builder = new StringBuilder();
        builder.append("# Shipment Feature Profiling Report\n\n");
        builder.append("- Generated at: `").append(generatedAt).append("`\n");
        builder.append("- Scope: all shipment API endpoints, security paths, database-backed service flows, ");
        builder.append("RabbitMQ consumers, and outbound event publisher paths\n");
        builder.append("- Result: `").append(passed).append('/').append(results.size()).append("` scenarios passed\n\n");
        builder.append("## Scenario Results\n\n");
        builder.append("| Area | Feature | Entry Point | Status | Runtime | Notes |\n");
        builder.append("| --- | --- | --- | --- | ---: | --- |\n");
        for (ShipmentFeatureProfileResult result : results) {
            builder.append("| ")
                    .append(markdownCell(result.area()))
                    .append(" | ")
                    .append(markdownCell(result.feature()))
                    .append(" | `")
                    .append(markdownCell(result.entryPoint()))
                    .append("` | ")
                    .append(result.status())
                    .append(" | ")
                    .append(result.durationMs())
                    .append(" ms | ")
                    .append(markdownCell(result.notes()))
                    .append(" |\n");
        }
        builder.append('\n');
        builder.append("## Notes\n\n");
        builder.append("- This is an integration profiling run on the local H2 test database, ");
        builder.append("not a Supabase production benchmark.\n");
        builder.append("- Replica consumer handlers are covered with mocked replica services because H2 ");
        builder.append("does not execute the PostgreSQL-specific replica upsert SQL used in production.\n");
        builder.append("- Use the same scenarios against staging with Supabase PostgreSQL, ");
        builder.append("RabbitMQ enabled, and realistic row counts before changing indexes or SLOs.\n");
        builder.append("- Runtime numbers are useful for comparing local regressions in this codebase; ");
        builder.append("absolute production latency must come from staging or production-like infrastructure.\n");
        return builder.toString();
    }

    private String html(String generatedAt, List<ShipmentFeatureProfileResult> results) {
        long passed = results.stream().filter(ShipmentFeatureProfileResult::success).count();
        StringBuilder builder = new StringBuilder();
        builder.append("""
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Shipment Feature Profiling Report</title>
                  <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; margin: 32px; color: #17202a; }
                    h1 { font-size: 28px; margin-bottom: 4px; }
                    .meta { color: #52616b; margin: 4px 0; }
                    .summary { margin: 20px 0; padding: 14px 16px; background: #f4f7f9; border: 1px solid #d7e0e7; border-radius: 6px; }
                    table { width: 100%; border-collapse: collapse; font-size: 13px; }
                    th, td { border: 1px solid #d7e0e7; padding: 9px 10px; text-align: left; vertical-align: top; }
                    th { background: #edf2f5; }
                    .pass { color: #176b3a; font-weight: 700; }
                    .fail { color: #a12828; font-weight: 700; }
                    code { background: #eef2f5; padding: 1px 4px; border-radius: 4px; }
                  </style>
                </head>
                <body>
                """);
        builder.append("<h1>Shipment Feature Profiling Report</h1>\n");
        builder.append("<p class=\"meta\">Generated at: <code>").append(escapeHtml(generatedAt)).append("</code></p>\n");
        builder.append("<p class=\"summary\">Result: <strong>")
                .append(passed)
                .append('/')
                .append(results.size())
                .append("</strong> scenarios passed. Scope includes API endpoints, security paths, service flows, ")
                .append("RabbitMQ consumers, and outbound event publisher paths.</p>\n");
        builder.append("<table><thead><tr><th>Area</th><th>Feature</th><th>Entry Point</th>");
        builder.append("<th>Status</th><th>Runtime</th><th>Notes</th></tr></thead><tbody>\n");
        for (ShipmentFeatureProfileResult result : results) {
            builder.append("<tr><td>")
                    .append(escapeHtml(result.area()))
                    .append("</td><td>")
                    .append(escapeHtml(result.feature()))
                    .append("</td><td><code>")
                    .append(escapeHtml(result.entryPoint()))
                    .append("</code></td><td class=\"")
                    .append(result.success() ? "pass" : "fail")
                    .append("\">")
                    .append(result.status())
                    .append("</td><td>")
                    .append(result.durationMs())
                    .append(" ms</td><td>")
                    .append(escapeHtml(result.notes()))
                    .append("</td></tr>\n");
        }
        builder.append("</tbody></table>\n");
        builder.append("""
                <p class="meta">Local H2 profiling is for regression comparison only. Replica consumer handlers use
                mocked replica services because H2 does not execute the PostgreSQL-specific replica upsert SQL used
                in production. Rerun against staging with Supabase PostgreSQL, RabbitMQ, and production-like data
                before setting performance SLOs.</p>
                </body>
                </html>
                """);
        return builder.toString();
    }

    private String markdownCell(String value) {
        return value.replace("|", "\\|").replace('\n', ' ');
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private void writePng(String generatedAt, List<ShipmentFeatureProfileResult> results) throws IOException {
        int height = 220 + ROW_HEIGHT * (results.size() + 1) + 80;
        BufferedImage image = new BufferedImage(REPORT_WIDTH, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, REPORT_WIDTH, height);
            drawHeader(graphics, generatedAt, results);
            drawTable(graphics, results);
        } finally {
            graphics.dispose();
        }
        ImageIO.write(image, "png", pngReportPath().toFile());
    }

    private void drawHeader(Graphics2D graphics, String generatedAt, List<ShipmentFeatureProfileResult> results) {
        long passed = results.stream().filter(ShipmentFeatureProfileResult::success).count();
        graphics.setColor(new Color(0xEEF4F7));
        graphics.fillRect(0, 0, REPORT_WIDTH, 130);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
        graphics.setColor(new Color(0x17202A));
        graphics.drawString("Shipment Feature Profiling Report", LEFT_MARGIN, 72);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        graphics.setColor(new Color(0x52616B));
        graphics.drawString(
                "Generated: " + generatedAt + "    Result: " + passed + "/" + results.size() + " scenarios passed",
                LEFT_MARGIN,
                108
        );
        graphics.drawString(
                "Local H2 integration profiling. Replica consumer handlers use mocked replica services.",
                LEFT_MARGIN,
                136
        );
    }

    private void drawTable(Graphics2D graphics, List<ShipmentFeatureProfileResult> results) {
        int[] widths = {150, 430, 390, 105, 115, 300};
        String[] headers = {"Area", "Feature", "Entry Point", "Status", "Runtime", "Notes"};
        int y = 160;
        drawRow(graphics, headers, widths, y, true, false);
        y += ROW_HEIGHT;
        for (int index = 0; index < results.size(); index++) {
            ShipmentFeatureProfileResult result = results.get(index);
            String[] values = {
                    result.area(),
                    result.feature(),
                    result.entryPoint(),
                    result.status(),
                    result.durationMs() + " ms",
                    result.notes()
            };
            drawRow(graphics, values, widths, y, false, index % 2 == 0);
            y += ROW_HEIGHT;
        }
    }

    private void drawRow(
            Graphics2D graphics,
            String[] values,
            int[] widths,
            int y,
            boolean header,
            boolean whiteBackground
    ) {
        graphics.setColor(header ? new Color(0xDFEAF0) : rowBackground(whiteBackground));
        graphics.fillRect(LEFT_MARGIN, y, REPORT_WIDTH - 2 * LEFT_MARGIN, ROW_HEIGHT);
        graphics.setFont(new Font(Font.SANS_SERIF, header ? Font.BOLD : Font.PLAIN, header ? 18 : 17));
        int x = LEFT_MARGIN;
        for (int index = 0; index < values.length; index++) {
            graphics.setColor(statusColor(index, values[index]));
            graphics.drawString(clip(values[index], maxChars(index)), x + 8, y + 25);
            graphics.setColor(new Color(0xD7E0E7));
            graphics.drawLine(x, y, x, y + ROW_HEIGHT);
            x += widths[index];
        }
        graphics.drawLine(REPORT_WIDTH - LEFT_MARGIN, y, REPORT_WIDTH - LEFT_MARGIN, y + ROW_HEIGHT);
        graphics.drawLine(LEFT_MARGIN, y + ROW_HEIGHT, REPORT_WIDTH - LEFT_MARGIN, y + ROW_HEIGHT);
    }

    private Color rowBackground(boolean whiteBackground) {
        return whiteBackground ? Color.WHITE : new Color(0xF8FAFB);
    }

    private Color statusColor(int index, String value) {
        if (index == 3 && "PASS".equals(value)) {
            return new Color(0x176B3A);
        }
        return new Color(0x17202A);
    }

    private int maxChars(int index) {
        int[] limits = {14, 48, 42, 8, 10, 28};
        return limits[index];
    }

    private String clip(String value, int maxChars) {
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars - 3) + "...";
    }
}
