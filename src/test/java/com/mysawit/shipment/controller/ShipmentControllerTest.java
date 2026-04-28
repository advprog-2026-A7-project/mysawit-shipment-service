package com.mysawit.shipment.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.mysawit.shipment.domain.ShipmentStatus;
import com.mysawit.shipment.dto.AdminApprovalRequest;
import com.mysawit.shipment.dto.CreateShipmentRequest;
import com.mysawit.shipment.dto.ShipmentResponse;
import com.mysawit.shipment.dto.UpdateStatusRequest;
import com.mysawit.shipment.exception.ShipmentForbiddenException;
import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.security.ShipmentSecurityAttributes;
import com.mysawit.shipment.service.ShipmentService;

class ShipmentControllerTest {

    private static final UUID ID_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ID_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID MANDOR_ID = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
    private static final UUID SUPIR_ID = UUID.fromString("bbbbbbbb-2222-2222-2222-222222222222");
    private static final UUID HARVEST_A = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final String DESTINATION = "Jakarta";

    private ShipmentService shipmentService;
    private ShipmentController shipmentController;

    @BeforeEach
    void setUp() {
        shipmentService = mock(ShipmentService.class);
        shipmentController = new ShipmentController(shipmentService);
    }

    @Test
    void getAllShipmentsReturnsServiceResult() {
        when(shipmentService.getAllShipments()).thenReturn(List.of(sampleShipment(ID_1), sampleShipment(ID_2)));

        ResponseEntity<List<ShipmentResponse>> response = shipmentController.getAllShipments(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        verify(shipmentService).getAllShipments();
    }

    @Test
    void getShipmentByIdReturnsShipmentResponse() {
        Shipment shipment = sampleShipment(ID_1);
        when(shipmentService.getShipmentById(ID_1)).thenReturn(shipment);

        ResponseEntity<ShipmentResponse> response = shipmentController.getShipmentById(ID_1, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ID_1, response.getBody().id());
        assertEquals(DESTINATION, response.getBody().destination());
        assertEquals(ShipmentStatus.MEMUAT, response.getBody().status());
    }

    @Test
    void getShipmentByIdPropagatesRuntimeExceptionWhenMissing() {
        when(shipmentService.getShipmentById(ID_1)).thenThrow(new RuntimeException("missing"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> shipmentController.getShipmentById(ID_1, null));

        assertEquals("missing", exception.getMessage());
    }

    @Test
    void healthReturnsUpStatus() {
        ResponseEntity<Map<String, String>> response = shipmentController.health();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("mysawit-shipment-service", response.getBody().get("service"));
    }

    @Test
    void createShipmentReturnsCreatedResponse() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                SUPIR_ID, DESTINATION,
                List.of(new CreateShipmentRequest.HarvestItem(HARVEST_A, 100.0)));

        Shipment saved = sampleShipment(ID_1);
        when(shipmentService.createShipment(eq(MANDOR_ID), any(CreateShipmentRequest.class)))
                .thenReturn(saved);

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setAttribute(ShipmentSecurityAttributes.JWT_USER_ID, MANDOR_ID);
        httpRequest.setAttribute(ShipmentSecurityAttributes.JWT_ROLE, "MANDOR");

        ResponseEntity<ShipmentResponse> response = shipmentController.createShipment(request, httpRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(ID_1, response.getBody().id());
        verify(shipmentService).createShipment(eq(MANDOR_ID), any(CreateShipmentRequest.class));
    }

    @Test
    void createShipmentRejectsNonMandorRole() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                SUPIR_ID, DESTINATION,
                List.of(new CreateShipmentRequest.HarvestItem(HARVEST_A, 100.0)));

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setAttribute(ShipmentSecurityAttributes.JWT_ROLE, "SUPIR");

        ShipmentForbiddenException exception = assertThrows(
                ShipmentForbiddenException.class,
                () -> shipmentController.createShipment(request, httpRequest)
        );

        assertEquals("Forbidden", exception.getMessage());
    }

    @Test
    void updateShipmentStatusReturnsOkResponse() {
        Shipment saved = sampleShipment(ID_1);
        saved.setStatus(ShipmentStatus.MENGIRIM);
        when(shipmentService.updateShipmentStatus(ID_1, SUPIR_ID, ShipmentStatus.MENGIRIM))
                .thenReturn(saved);

        ResponseEntity<ShipmentResponse> response = shipmentController.updateShipmentStatus(
                ID_1,
                new UpdateStatusRequest("MENGIRIM"),
                supirRequest()
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ShipmentStatus.MENGIRIM, response.getBody().status());
        verify(shipmentService).updateShipmentStatus(ID_1, SUPIR_ID, ShipmentStatus.MENGIRIM);
    }

    @Test
    void updateShipmentStatusRejectsBlankStatus() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> shipmentController.updateShipmentStatus(
                        ID_1,
                        new UpdateStatusRequest(" "),
                        supirRequest()
                )
        );

        assertEquals("Invalid status value", exception.getMessage());
    }

    @Test
    void approveShipmentByAdminReturnsOkResponse() {
        AdminApprovalRequest request = new AdminApprovalRequest("ADMIN_APPROVED");

        Shipment saved = sampleShipment(ID_1);
        saved.setStatus(ShipmentStatus.ADMIN_APPROVED);
        when(shipmentService.approveShipmentByAdmin(ID_1, ShipmentStatus.ADMIN_APPROVED))
                .thenReturn(saved);

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setAttribute(ShipmentSecurityAttributes.JWT_ROLE, "ADMIN");

        ResponseEntity<ShipmentResponse> response =
                shipmentController.approveShipmentByAdmin(ID_1, request, httpRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ShipmentStatus.ADMIN_APPROVED, response.getBody().status());
        verify(shipmentService).approveShipmentByAdmin(ID_1, ShipmentStatus.ADMIN_APPROVED);
    }

    @Test
    void approveShipmentByAdminRejectsUnknownStatus() {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setAttribute(ShipmentSecurityAttributes.JWT_ROLE, "ADMIN");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> shipmentController.approveShipmentByAdmin(
                        ID_1,
                        new AdminApprovalRequest("UNKNOWN"),
                        httpRequest
                )
        );

        assertEquals("Invalid status value", exception.getMessage());
    }

    private MockHttpServletRequest supirRequest() {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setAttribute(ShipmentSecurityAttributes.JWT_USER_ID, SUPIR_ID);
        httpRequest.setAttribute(ShipmentSecurityAttributes.JWT_ROLE, "SUPIR");
        return httpRequest;
    }

    private Shipment sampleShipment(UUID id) {
        Shipment shipment = new Shipment();
        shipment.setId(id);
        shipment.setMandorUserId(UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111"));
        shipment.setSupirUserId(UUID.fromString("bbbbbbbb-2222-2222-2222-222222222222"));
        shipment.setDestination(DESTINATION);
        shipment.setTotalKg(100.0);
        shipment.setStatus(ShipmentStatus.MEMUAT);
        return shipment;
    }
}
