/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/**/*.{html,ts}"],
  theme: {
    extend: {
      colors: {
        brand: {
          green: '#0F2F24',
          gold: '#E6B85C',
          white: '#F7F3EA',
        }
      }
    }
  },
  plugins: [],
}
