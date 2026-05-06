import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const args = ['--run', '--passWithNoTests'];
let hasCoverageFlag = false;

for (const arg of process.argv.slice(2)) {
  if (arg === '--watchAll' || arg.startsWith('--watchAll=')) {
    continue;
  }

  if (arg === '--coverage') {
    hasCoverageFlag = true;
  }

  args.push(arg);
}

if (!hasCoverageFlag) {
  args.push('--coverage');
}

const vitestEntry = fileURLToPath(new URL('../node_modules/vitest/vitest.mjs', import.meta.url));

const result = spawnSync(process.execPath, [vitestEntry, ...args], {
  stdio: 'inherit',
});

process.exit(result.status ?? 1);