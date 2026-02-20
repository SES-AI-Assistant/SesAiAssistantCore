package copel.sesproductpackage.core.api.aws;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;

class ParameterStoreTests {

    private MockedStatic<SsmClient> mockedClient;
    private SsmClient mockSsmClient;

    @BeforeEach
    void setUp() {
        mockedClient = mockStatic(SsmClient.class);
        mockSsmClient = mock(SsmClient.class);
        SsmClientBuilder mockBuilder = mock(SsmClientBuilder.class);
        when(mockBuilder.region(any())).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockSsmClient);
        mockedClient.when(SsmClient::builder).thenReturn(mockBuilder);
    }

    @AfterEach
    void tearDown() {
        mockedClient.close();
    }

    @Test
    void testGetParameter() {
        ParameterStore ps = new ParameterStore(Region.AP_NORTHEAST_1);
        
        GetParameterResponse mockResponse = GetParameterResponse.builder()
                .parameter(Parameter.builder().value("secret-value").build())
                .build();
        when(mockSsmClient.getParameter(any(GetParameterRequest.class))).thenReturn(mockResponse);
        
        assertEquals("secret-value", ps.getParameter("test-key"));
        assertEquals("secret-value", ps.getParameter("test-key", false));
        
        verify(mockSsmClient, times(2)).getParameter(any(GetParameterRequest.class));
    }
}
