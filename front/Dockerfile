FROM node:18-alpine
WORKDIR /app
ENV NODE_OPTIONS=--openssl-legacy-provider
COPY package.json ./
RUN npm install --legacy-peer-deps
COPY . .
EXPOSE 3000
CMD ["npm", "start"]