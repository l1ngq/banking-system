import { currencyRates } from '../data/mockData';
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

const DEFAULT_RATES: Array<{ baseCurrency: 'USD' | 'EUR' | 'RUB'; targetCurrency: 'USD' | 'EUR' | 'RUB'; rate: number }> = [
  { baseCurrency: 'USD', targetCurrency: 'RUB', rate: 90 },
  { baseCurrency: 'RUB', targetCurrency: 'USD', rate: 0.011111 },
  { baseCurrency: 'EUR', targetCurrency: 'RUB', rate: 98 },
  { baseCurrency: 'RUB', targetCurrency: 'EUR', rate: 0.010204 },
  { baseCurrency: 'USD', targetCurrency: 'EUR', rate: 0.918367 },
  { baseCurrency: 'EUR', targetCurrency: 'USD', rate: 1.088889 },
];

function ensureBackendCurrency(currency: CurrencyCode) {
  if (!['USD', 'EUR', 'RUB'].includes(currency)) {
    throw new Error('Доступны только валюты USD, EUR и RUB');
  }
  return currency as 'USD' | 'EUR' | 'RUB';
}

function mergeRate(code: 'USD' | 'EUR', rate: number): CurrencyRate {
  const fallback = currencyRates.find((item) => item.code === code);
  return {
    code,
    name: fallback?.name ?? code,
    buy: Number((rate * 0.985).toFixed(4)),
    sell: Number(rate.toFixed(4)),
    change: fallback?.change ?? 0,
  };
}

export const currencyApi = {
  async seedDefaultRates() {
    await Promise.all(
      DEFAULT_RATES.map((rate) =>
        request<unknown>(`${API_URLS.currencies}/api/currencies/rates`, {
          method: 'PUT',
          body: JSON.stringify(rate),
        }),
      ),
    );
  },

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

    return [mergeRate('USD', usdRub), mergeRate('EUR', eurRub)];
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
