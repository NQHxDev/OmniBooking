import http from 'k6/http';
import { check, sleep } from 'k6';
import { htmlReport } from "https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js";
import { textSummary } from "https://jslib.k6.io/k6-summary/0.0.1/index.js";

// Configures load test execution stages and SLA thresholds
export const options = {
   // Ramps VUs up, sustains load, and then ramps down
   stages: [
      { duration: '30s', target: 50 },  // Ramps up VUs from 0 to 50
      { duration: '1m', target: 50 },   // Sustains load at 50 VUs
      { duration: '15s', target: 0 },   // Ramps down VUs back to 0
   ],

   // SLA criteria; failures mark the load test run as failed
   thresholds: {
      http_req_failed: ['rate<0.01'],    // Less than 1% request failure rate
      http_req_duration: ['p(95)<300'], // 95% of requests must complete under 300ms
   },
};

// Base URL for API endpoints
const BASE_URL = __ENV.API_URL || 'http://localhost:8080/api/v1';

// Popular destinations in Vietnam to simulate search queries
const DESTINATIONS = [
   'Ha Noi',
   'TP-HCM',
   'Da Nang',
   'Nha Trang',
   'Phu Quoc',
   'Da Lat',
   'Sapa',
   'Vung Tau',
   'Hoi An',
   'Ha Long'
];

export default function () {
   // Step 1: Health check baseline (lightweight, no database queries)
   let healthRes = http.get(`${BASE_URL}/health`);
   check(healthRes, {
      'health status is 200': (r) => r.status === 200,
      'health check reports UP': (r) => {
         try {
         const body = JSON.parse(r.body);
         return body.data && body.data.status === 'UP';
         } catch (e) {
         return false;
         }
      },
   });
   sleep(1); // Simulates user think time

   // Step 2: Fetch featured properties (simulates home page load)
   let featuredRes = http.get(`${BASE_URL}/properties/featured?limit=6`);
   check(featuredRes, {
      'featured properties status is 200': (r) => r.status === 200,
      'featured properties list is returned': (r) => {
         try {
         const body = JSON.parse(r.body);
         return body.message === 'Success' && Array.isArray(body.data);
         } catch (e) {
         return false;
         }
      },
   });
   sleep(1.5);

   // Step 3: Fetch new properties (simulates home page load)
   let newRes = http.get(`${BASE_URL}/properties/new?limit=15`);
   check(newRes, {
      'new properties status is 200': (r) => r.status === 200,
      'new properties list is returned': (r) => {
         try {
         const body = JSON.parse(r.body);
         return body.message === 'Success' && Array.isArray(body.data);
         } catch (e) {
         return false;
         }
      },
   });
   sleep(2);

   // Step 4: Search properties (simulates a user searching for a city)
   const randomCity = DESTINATIONS[Math.floor(Math.random() * DESTINATIONS.length)];

   let searchRes = http.get(`${BASE_URL}/properties/search?ss=${encodeURIComponent(randomCity)}&minPrice=10&maxPrice=500&group_adults=2&group_children=0&no_rooms=1&page=0&size=10`);
   check(searchRes, {
      'search status is 200': (r) => r.status === 200,
      'search results received': (r) => {
         try {
         const body = JSON.parse(r.body);
         return body.message === 'Success' && body.data && Array.isArray(body.data.content);
         } catch (e) {
         return false;
         }
      },
   });

   // Views details of a random hotel from search results if available
   try {
      const searchBody = JSON.parse(searchRes.body);
      if (searchBody.message === 'Success' && searchBody.data && searchBody.data.content.length > 0) {
         sleep(2.5); // Simulates reading search results

         const hotels = searchBody.data.content;
         const randomHotel = hotels[Math.floor(Math.random() * hotels.length)];

         let detailRes = http.get(`${BASE_URL}/properties/${randomHotel.id}`);
         check(detailRes, {
         'detail status is 200': (r) => r.status === 200,
         'detail data is valid': (r) => {
            try {
               const body = JSON.parse(r.body);
               return body.message === 'Success' && body.data && body.data.id === randomHotel.id;
            } catch (e) {
               return false;
            }
         },
         });
      }
   } catch (e) {
      // Ignores parsing exceptions from bad request payloads or error statuses
   }

   sleep(3); // Simulates final think time before loop iteration
}

export function handleSummary(data) {
   return {
      "Server/load-tests/summary.html": htmlReport(data),
      "Server/load-tests/summary.txt": textSummary(data, { indent: " ", enableColors: false }),
      stdout: textSummary(data, { indent: " ", enableColors: true }),
   };
}
