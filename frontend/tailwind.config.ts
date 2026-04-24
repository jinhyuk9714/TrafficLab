import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./src/app/**/*.{ts,tsx}",
    "./src/components/**/*.{ts,tsx}",
    "./src/lib/**/*.{ts,tsx}"
  ],
  theme: {
    extend: {
      colors: {
        ink: "#171717",
        panel: "#ffffff",
        line: "#dedbd2",
        mint: "#0f9f84",
        coral: "#e85d48",
        amber: "#d9952f"
      },
      boxShadow: {
        panel: "0 18px 40px rgba(23, 23, 23, 0.08)"
      }
    }
  },
  plugins: []
};

export default config;
