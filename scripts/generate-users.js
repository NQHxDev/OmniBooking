import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import { fakerVI } from "@faker-js/faker";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const NUM_USERS = 10000;
const OUTPUT_FILE = path.join(
   __dirname,
   "..",
   "Server",
   "src",
   "main",
   "resources",
   "mock-users.json"
);

console.log(`Generating ${NUM_USERS} mock users...`);

function removeDiacritics(str) {
   return str
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .replace(/[đĐ]/g, (char) => (char === "đ" ? "d" : "D"));
}

const usernameSet = new Set();

function generateCleanUsername(firstName, lastName) {
   const normFirst = removeDiacritics(firstName).toLowerCase().replace(/\s+/g, "");
   const normLast = removeDiacritics(lastName).toLowerCase().replace(/\s+/g, "");
   const baseUsername = `${normFirst}${normLast}`;

   let username = baseUsername;
   if (usernameSet.has(username)) {
      let counter = 1;
      while (usernameSet.has(`${baseUsername}${counter}`)) {
         counter++;
      }
      username = `${baseUsername}${counter}`;
   }
   usernameSet.add(username);

   return username;
}

const users = [];
const genders = ["MALE", "FEMALE"];
const cities = [
   "Hà Nội",
   "Thành Phố Hồ Chí Minh",
   "Đà Nẵng",
   "Hội An",
   "Phú Quốc",
   "Hạ Long",
   "Nha Trang",
   "Đà Lạt",
   "Vũng Tàu",
];

for (let i = 1; i <= NUM_USERS; i++) {
   const gender = genders[Math.floor(Math.random() * genders.length)];
   const city = cities[Math.floor(Math.random() * cities.length)];

   const fakerGender = gender === "MALE" ? "male" : "female";
   const firstName = fakerVI.person.firstName(fakerGender);
   const lastName = fakerVI.person.lastName();
   const displayName = `${lastName} ${firstName}`;
   const username = generateCleanUsername(firstName, lastName);
   const email = `${username}@omnibooking.com`;

   users.push({
      username: username,
      email: email,
      displayName: displayName,
      gender: gender,
      address: `Đường số ${i}, ${city}, Việt Nam`,
      nationality: "Việt Nam",
   });
}

// Ensure directory exists
const dir = path.dirname(OUTPUT_FILE);
if (!fs.existsSync(dir)) {
   fs.mkdirSync(dir, { recursive: true });
}

fs.writeFileSync(OUTPUT_FILE, JSON.stringify(users, null, 3), "utf-8");
console.log(`Successfully generated ${NUM_USERS} mock users and wrote to Resources`);
