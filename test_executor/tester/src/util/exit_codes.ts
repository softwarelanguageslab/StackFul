/**
 * enum of exit codes for main program
 * @type {Readonly<{unknownError: number, success: number, testTracerAssertionFailed: number, applicationNotFound: number, communicationError: number}>}
 */
const enum ExitCodes {
  success=                		0,
  unknownError=           		1,
  communicationError=     		2,
  applicationNotFound=    		3,
  testTracerAssertionFailed= 	10,
};

export default ExitCodes;