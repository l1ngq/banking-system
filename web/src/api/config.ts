export const API_URLS = {
  core: import.meta.env.VITE_CORE_API_URL ?? '/core-api',
  currencies: import.meta.env.VITE_CURRENCIES_API_URL ?? '/currencies-api',
  info: import.meta.env.VITE_INFO_API_URL ?? '',
  assistant: import.meta.env.VITE_ASSISTANT_API_URL ?? '',
};

export function getDisplayNameByEmail(email?: string | null, fallback = 'Пользователь') {
  if (!email) return fallback;

  const prefix = email.split('@')[0]?.trim();
  if (!prefix) return fallback;

  return prefix
    .split(/[._-]+/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}
