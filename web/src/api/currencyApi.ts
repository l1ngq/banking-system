import type { CurrencyCode, CurrencyRate } from '../types/banking';
import { API_URLS } from './config';
import { request } from './http';

type BackendRate = {
  from: string;
  to: string;
  rate: number | string;
  timestamp?: string;
};

type BackendConversion = {
  from: string;
  to: string;
  amount: number | string;
  convertedAmount: number | string;
  rate: number | string;
};

function ensureBackendCurrency(currency: CurrencyCode) {
  if (!['USD', 'EUR', 'RUB'].includes(currency)) {
    throw new Error('Доступны только валюты USD, EUR и RUB');
  }
  return currency as 'USD' | 'EUR' | 'RUB';
}

function rateName(code: 'USD' | 'EUR') {
  return code === 'USD' ? 'Доллар США' : 'Евро';
}

function mapRate(code: 'USD' | 'EUR', rate: number): CurrencyRate {
  return {
    code,
    name: rateName(code),
    buy: Number((rate * 0.985).toFixed(4)),
    sell: Number(rate.toFixed(4)),
    change: 0,
  };
}

export const currencyApi = {
  async getRate(from: CurrencyCode, to: CurrencyCode) {
    const rate = await request<BackendRate>(
      `${API_URLS.currencies}/api/currencies/rate?from=${encodeURIComponent(
        ensureBackendCurrency(from),
      )}&to=${encodeURIComponent(ensureBackendCurrency(to))}`,
    );
    return Number(rate.rate);
  },

  async getRates(): Promise<CurrencyRate[]> {
    const [usdRub, eurRub] = await Promise.all([
      this.getRate('USD', 'RUB'),
      this.getRate('EUR', 'RUB'),
    ]);

    return [mapRate('USD', usdRub), mapRate('EUR', eurRub)];
  },

  async convert(payload: { from: CurrencyCode; to: CurrencyCode; amount: number }) {
    const result = await request<BackendConversion>(
      `${API_URLS.currencies}/api/currencies/convert?from=${encodeURIComponent(
        ensureBackendCurrency(payload.from),
      )}&to=${encodeURIComponent(ensureBackendCurrency(payload.to))}&amount=${encodeURIComponent(payload.amount)}`,
    );

    return { result: Number(result.convertedAmount), rate: Number(result.rate) };
  },
};
