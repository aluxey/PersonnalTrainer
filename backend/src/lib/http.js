import { config } from './config.js';

export function requireApiKey(req, res, next) {
  const header = req.get('x-api-key');
  if (!header || header !== config.ingestApiKey) {
    return res.status(401).json({ error: 'invalid_api_key' });
  }
  return next();
}

export function asyncRoute(handler) {
  return async (req, res, next) => {
    try {
      await handler(req, res, next);
    } catch (error) {
      next(error);
    }
  };
}

export function errorHandler(error, req, res, next) {
  if (res.headersSent) {
    return next(error);
  }

  const status = error.statusCode || error.status || 500;
  const payload = {
    error: error.code || 'internal_error',
    message: status >= 500 ? 'Unexpected server error' : error.message
  };

  if (process.env.NODE_ENV !== 'production') {
    payload.detail = error.message;
  }

  return res.status(status).json(payload);
}
