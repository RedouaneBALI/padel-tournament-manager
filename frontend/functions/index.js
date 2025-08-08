// frontend/functions/index.js
const { onRequest } = require('firebase-functions/v2/https');
const { defineSecret } = require('firebase-functions/params');
const next = require('next');

const NEXTAUTH_SECRET = defineSecret('NEXTAUTH_SECRET');
const GOOGLE_CLIENT_ID = defineSecret('GOOGLE_CLIENT_ID');
const GOOGLE_CLIENT_SECRET = defineSecret('GOOGLE_CLIENT_SECRET');
const NEXTAUTH_URL = defineSecret('NEXTAUTH_URL');

const app = next({ dev: process.env.NODE_ENV !== 'production', conf: { distDir: '.next' } });
const handle = app.getRequestHandler();
let prepared;

exports.nextApp = onRequest(
  { timeoutSeconds: 540, memory: '1GiB', secrets: [NEXTAUTH_SECRET, GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, NEXTAUTH_URL] },
  async (req, res) => {
    if (!prepared) {
      process.env.NEXTAUTH_SECRET = NEXTAUTH_SECRET.value();
      process.env.GOOGLE_CLIENT_ID = GOOGLE_CLIENT_ID.value();
      process.env.GOOGLE_CLIENT_SECRET = GOOGLE_CLIENT_SECRET.value();
      process.env.NEXTAUTH_URL = NEXTAUTH_URL.value();
      prepared = app.prepare();
    }
    await prepared;

    // ⚠️ ne JAMAIS cloner req — on passe l’objet Node natif
    return handle(req, res);
  }
);
