export const API_URLS = {
  core: import.meta.env.VITE_CORE_API_URL ?? '/api',
  currencies: import.meta.env.VITE_CURRENCIES_API_URL ?? '/api/currencies',
  info: import.meta.env.VITE_INFO_API_URL ?? '',
  assistant: import.meta.env.VITE_ASSISTANT_API_URL ?? '',
};

export type DemoUser = {
  name: string;
  email: string;
  password: string;
};

export const DEMO_USERS: DemoUser[] = [
  { name: 'Игорь', email: 'igor@example.com', password: 'password123' },
  { name: 'Максим', email: 'maxim@example.com', password: 'password123' },
  { name: 'Николай', email: 'nikolay@example.com', password: 'password123' },
];

const fallbackDemoUser = DEMO_USERS[0];

export const DEMO_AUTH = {
  enabled: (import.meta.env.VITE_AUTO_LOGIN ?? 'true') !== 'false',
  name: fallbackDemoUser.name,
  email: import.meta.env.VITE_DEMO_EMAIL ?? fallbackDemoUser.email,
  password: import.meta.env.VITE_DEMO_PASSWORD ?? fallbackDemoUser.password,
};

export function getDemoUserByEmail(email?: string | null): DemoUser | undefined {
  if (!email) return undefined;
  return DEMO_USERS.find((user) => user.email.toLowerCase() === email.toLowerCase());
}

export function getDisplayNameByEmail(email?: string | null, fallback = 'Пользователь') {
  const demoUser = getDemoUserByEmail(email);
  if (demoUser) return demoUser.name;

  if (!email) return fallback;
  const prefix = email.split('@')[0]?.trim();
  if (!prefix) return fallback;

  return prefix
    .split(/[._-]+/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}
