package in.dipesh.spring_backend.service;

import in.dipesh.spring_backend.dto.KhaltiInitiateApiResponse;
import in.dipesh.spring_backend.dto.KhaltiInitiateRequest;
import in.dipesh.spring_backend.dto.KhaltiLookupResponse;

public interface KhaltiApiClient {
    KhaltiInitiateApiResponse initiate(KhaltiInitiateRequest request);
    KhaltiLookupResponse lookup(String pidx);
}
