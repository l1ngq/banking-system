interface ImportMetaEnv {
  readonly VITE_CORE_API_URL?: string;
  readonly VITE_CURRENCIES_API_URL?: string;
  readonly VITE_INFO_API_URL?: string;
  readonly VITE_ASSISTANT_API_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

declare module '*.css';
