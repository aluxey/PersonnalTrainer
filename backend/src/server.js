import express from 'express';
import cors from 'cors';
import { config } from './lib/config.js';
import { errorHandler } from './lib/http.js';
import { ingestRouter } from './routes/ingest.js';
import { jobsRouter } from './routes/jobs.js';
import { startScheduler } from './jobs/scheduler.js';

const app = express();

app.use(cors());
app.use(express.json({ limit: '1mb' }));

app.get('/.well-known/appspecific/com.chrome.devtools.json', (req, res) => {
  res.status(204).end();
});

app.get('/', (req, res) => {
  res.json({
    ok: true,
    service: 'personal-trainer-backend',
    health: '/health',
    ingest: '/api/ingest/health-connect'
  });
});

app.get('/health', (req, res) => {
  res.json({ ok: true, service: 'personal-trainer-backend' });
});

app.use('/api/ingest', ingestRouter);
app.use('/api/jobs', jobsRouter);
app.use(errorHandler);

app.listen(config.port, config.host, () => {
  console.log(`Backend listening on http://${config.host}:${config.port}`);
  startScheduler();
});
