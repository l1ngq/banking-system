export const assistantApi = {
  async ask(message: string) {
    return {
      answer: `Я могу подсказать информацию по вашим счетам и операциям. Ваш вопрос: ${message}`,
    };
  },
};
