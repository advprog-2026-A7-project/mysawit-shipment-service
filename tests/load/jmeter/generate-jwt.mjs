#!/usr/bin/env node
/**
 * Generate HS256 JWTs for shipment-service load testing.
 *
 * Reads JWT_SECRET from ../../../.env (shipment-service root) by default,
 * or from JWT_SECRET env var.
 *
 * Output (stdout):
 *   JWT_MANDOR=<token>
 *   JWT_SUPIR=<token>
 *   JWT_ADMIN=<token>
 *   MANDOR_USER_ID=<uuid>
 *   SUPIR_USER_ID=<uuid>
 *   ADMIN_USER_ID=<uuid>
 *
 * IDs are aligned with src/main/resources/sql/shipment-cleanup-and-seed.sql
 * so the SUPIR token sees the seeded shipments.
 */
import { createHmac } from 'node:crypto';
import { readFileSync, existsSync } from 'node:fs';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const SHIPMENT_ROOT = resolve(__dirname, '..', '..', '..');
const ENV_FILE = resolve(SHIPMENT_ROOT, '.env');

function loadJwtSecret() {
  if (process.env.JWT_SECRET && process.env.JWT_SECRET.trim().length > 0) {
    return process.env.JWT_SECRET;
  }
  if (!existsSync(ENV_FILE)) {
    throw new Error(`JWT_SECRET not set and ${ENV_FILE} not found`);
  }
  const lines = readFileSync(ENV_FILE, 'utf8').split('\n');
  for (const raw of lines) {
    const line = raw.trim();
    if (!line || line.startsWith('#')) continue;
    const eq = line.indexOf('=');
    if (eq === -1) continue;
    const key = line.slice(0, eq).trim();
    let value = line.slice(eq + 1).trim();
    if ((value.startsWith('"') && value.endsWith('"')) ||
        (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1);
    }
    if (key === 'JWT_SECRET') return value;
  }
  throw new Error(`JWT_SECRET not found in ${ENV_FILE}`);
}

function b64url(input) {
  return Buffer.from(input)
    .toString('base64')
    .replaceAll('+', '-')
    .replaceAll('/', '_')
    .replaceAll('=', '');
}

function signJwt(payload, secret) {
  const header = b64url(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const body = b64url(JSON.stringify(payload));
  const sig = b64url(createHmac('sha256', secret).update(`${header}.${body}`).digest());
  return `${header}.${body}.${sig}`;
}

const SECRET = loadJwtSecret();

// IDs sesuai seed shipment-cleanup-and-seed.sql + dev users.
const USERS = {
  MANDOR: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
  SUPIR:  'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
  ADMIN:  'cccccccc-3333-3333-3333-333333333333',
};

const now = Math.floor(Date.now() / 1000);
const exp = now + 60 * 60 * 8; // 8 jam

const tokens = {};
for (const [role, sub] of Object.entries(USERS)) {
  tokens[role] = signJwt({ sub, role, iat: now, exp }, SECRET);
}

// Print sebagai key=value supaya gampang di-source dari shell.
process.stdout.write(`JWT_MANDOR=${tokens.MANDOR}\n`);
process.stdout.write(`JWT_SUPIR=${tokens.SUPIR}\n`);
process.stdout.write(`JWT_ADMIN=${tokens.ADMIN}\n`);
process.stdout.write(`MANDOR_USER_ID=${USERS.MANDOR}\n`);
process.stdout.write(`SUPIR_USER_ID=${USERS.SUPIR}\n`);
process.stdout.write(`ADMIN_USER_ID=${USERS.ADMIN}\n`);
