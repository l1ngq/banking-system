import type { CurrencyCode } from '../types/banking';

const currencyMap: Record<CurrencyCode, string> = { RUB: '₽', USD: '$', EUR: '€' };

export function getCurrencySymbol(currency: CurrencyCode) { return currencyMap[currency]; }

export function formatMoney(amount: number, currency: CurrencyCode) {
  return `${amount.toLocaleString('ru-RU', { minimumFractionDigits: Math.abs(amount % 1) === 0 ? 0 : 2, maximumFractionDigits: 2 })} ${getCurrencySymbol(currency)}`;
}

export function formatCompactMoney(amount: number, currency: CurrencyCode = 'RUB') {
  return `${new Intl.NumberFormat('ru-RU', { notation: 'compact', maximumFractionDigits: 1 }).format(amount)} ${getCurrencySymbol(currency)}`;
}

export function formatDate(value: string) { return new Date(value).toLocaleDateString('ru-RU', { day: '2-digit', month: 'short', year: 'numeric' }); }
export function formatDateTime(value: string) { return new Date(value).toLocaleString('ru-RU', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' }); }
export function maskAccountNumber(value: string) { return `${value.slice(0, 4)} •••• •••• ${value.slice(-4)}`; }
export function getInitials(fullName: string) { return fullName.split(' ').filter(Boolean).slice(0, 2).map((part) => part[0]).join('').toUpperCase(); }
