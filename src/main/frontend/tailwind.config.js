/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    '../resources/templates/**/*.html'
  ],
  darkMode: 'class',
  theme: {
    extend: {
      screens: {
        xs: '375px'
      }
    }
  },
  plugins: [
    require('@tailwindcss/forms')
  ]
};
