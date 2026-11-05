package ru.example.moneytransfer.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import ru.example.moneytransfer.service.TransferService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@WebMvcTest(TransferController.class)
class TransferControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransferService transferService;   // ← заглушка вместо настоящего бина

    private String validTransferBody;
    private String unknownOpIdBody;

    @BeforeEach
    void setUp() {
        validTransferBody = """
                {
                  "cardFromNumber": "4111111111111111",
                  "cardFromValidTill": "12/27",
                  "cardFromCVV": "123",
                  "cardToNumber": "5555555555554444",
                  "amount": {
                    "value": 1000,
                    "currency": "RUR"
                  }
                }
                """;

        unknownOpIdBody = """
                {
                  "operationId": "deadbeef-0000-0000-0000-000000000000",
                  "code": "0000"
                }
                """;
    }

    @Test
    void transfer_success_returns_200_and_operationId() throws Exception {
        when(transferService.transfer(org.mockito.ArgumentMatchers.any()))
                .thenReturn("test-operation-id");

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/transfer")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validTransferBody)
                )
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.operationId").value("test-operation-id"));
    }

    @Test
    void confirmOperation_unknownId_returns_400_with_errorFormat() throws Exception {
        when(transferService.confirmOperation(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new ru.example.moneytransfer.exception.OperationNotFoundException(
                        "Операция не найдена: deadbeef-0000-0000-0000-000000000000"));

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/confirmOperation")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(unknownOpIdBody)
                )
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").exists());
    }
}