import { news } from '../data/mockData';

export const infoApi = {
  async getNews() {
    return news;
  },
};
