/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        neon: '#10b981',
        aurora: '#38bdf8',
        dark: {
          900: '#000000',
          800: '#0d1117',
          700: '#161b22',
          600: '#1c2333',
        }
      },
      animation: {
        'pulse-slow': 'pulse 3s ease-in-out infinite',
        'ripple': 'ripple 1.5s ease-out infinite',
        'glow': 'glow 2s ease-in-out infinite alternate',
        'spin-slow': 'spin 2s linear infinite',
      },
      keyframes: {
        ripple: {
          '0%': { transform: 'scale(1)', opacity: '0.6' },
          '100%': { transform: 'scale(2.5)', opacity: '0' },
        },
        glow: {
          '0%': { boxShadow: '0 0 20px rgba(16,185,129,0.3)' },
          '100%': { boxShadow: '0 0 40px rgba(16,185,129,0.6)' },
        },
      },
    },
  },
  plugins: [],
}
