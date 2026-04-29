import dotenv from 'dotenv';

dotenv.config({ path: new URL('../../../.env', import.meta.url) });
dotenv.config({ path: new URL('../../.env', import.meta.url) });
dotenv.config();

function readRequired(name) {
  const value = process.env[name];
  if (!value) {
    throw new Error(`Missing required environment variable ${name}`);
  }
  return value;
}

export const config = {
  port: Number(process.env.PORT || 8787),
  host: process.env.HOST || '127.0.0.1',
  timezone: process.env.TZ || 'Europe/Paris',
  supabaseUrl: readRequired('SUPABASE_URL'),
  supabaseServiceRoleKey: readRequired('SUPABASE_SERVICE_ROLE_KEY'),
  ingestApiKey: readRequired('INGEST_API_KEY'),
  runCron: process.env.RUN_CRON !== 'false',
  dailyCron: process.env.DAILY_CRON || '20 8 * * *',
  weeklyReportCron: process.env.WEEKLY_REPORT_CRON || '0 9 * * 0'
};
