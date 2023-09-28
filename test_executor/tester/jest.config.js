/** @type {import('ts-jest/dist/types').InitialOptionsTsJest} */
module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'node',
  modulePathIgnorePatterns: ["<rootDir>/tests"], /* Exclude the tests in this folder. Only execute the compiled tests in the built folder */
};