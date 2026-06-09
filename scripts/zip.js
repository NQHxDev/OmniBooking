import fs from "fs";
import path from "path";
import crypto from "crypto";
import { execSync } from "child_process";

const args = process.argv.slice(2);
if (args.length < 3) {
   console.error("Usage: node scripts/zip.js <output_dir> <base_name> [options] <dir1> [dir2] ...");
   process.exit(1);
}

const outDir = path.resolve(args[0]);
const baseName = args[1];
const remainingArgs = args.slice(2);

const excludes = [];
const inputPaths = [];

for (const arg of remainingArgs) {
   if (arg.startsWith("--exclude=")) {
      excludes.push(arg);
   } else {
      inputPaths.push(arg);
   }
}

if (!fs.existsSync(outDir)) {
   fs.mkdirSync(outDir, { recursive: true });
}

try {
   const files = fs.readdirSync(outDir);
   const prefix = `${baseName}_`;
   for (const file of files) {
      if (file.startsWith(prefix) && file.endsWith(".zip")) {
         const filePath = path.join(outDir, file);
         fs.unlinkSync(filePath);
         console.log(`Deleted existing archive: ${file}`);
      }
   }
} catch (err) {
   console.error(`Error cleaning up old archives: ${err.message}`);
   process.exit(1);
}

const uuid = crypto.randomUUID();
const parts = uuid.split("-");
const p0 = parts[0];
const p1 = parts[1].substring(0, 2);
const p2 = parts[2].substring(0, 2);
const num0 = parseInt(p0, 16);
const num1 = parseInt(p1, 16);
const num2 = parseInt(p2, 16);
const shortUuid = ((num0 ^ num1 ^ num2) >>> 0).toString(16).padStart(8, "0");
const outFilename = `${baseName}_${shortUuid}.zip`;
const outputFile = path.join(outDir, outFilename);
const relativeOutPath = path.relative(process.cwd(), outputFile).replace(/\\/g, "/");

try {
   console.log(`Creating ${outFilename}...`);
   const quotedInputs = inputPaths.map((p) => `"${p.replace(/\\/g, "/")}"`).join(" ");
   const excludeFlags = excludes.map((e) => `"${e.replace(/\\/g, "/")}"`).join(" ");
   const cmd = `tar ${excludeFlags} -caf "${relativeOutPath}" ${quotedInputs}`;
   execSync(cmd, { stdio: "inherit" });
   console.log(`Successfully created ${outFilename}`);
} catch (err) {
   console.error(`Error creating zip archive: ${err.message}`);
   console.error('Make sure you have "tar" installed (pre-installed on Windows 10+ and macOS)!');
   process.exit(1);
}
