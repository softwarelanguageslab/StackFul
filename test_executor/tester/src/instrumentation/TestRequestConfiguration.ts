export interface ReceivedRequest {
    OriginalURL: string;
    URLContainingCode: string;
}

var receivedRequest: ReceivedRequest | undefined = undefined;

export function setRequest(newRequest: ReceivedRequest): void {
    receivedRequest = newRequest;
}

export function getRequest(): ReceivedRequest | undefined {
    return receivedRequest;
}