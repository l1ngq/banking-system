FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .

ARG VITE_CORE_API_URL
ARG VITE_CURRENCIES_API_URL
ARG VITE_INFO_API_URL
ARG VITE_ASSISTANT_API_URL

ENV VITE_CORE_API_URL=$VITE_CORE_API_URL
ENV VITE_CURRENCIES_API_URL=$VITE_CURRENCIES_API_URL
ENV VITE_INFO_API_URL=$VITE_INFO_API_URL
ENV VITE_ASSISTANT_API_URL=$VITE_ASSISTANT_API_URL

RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
