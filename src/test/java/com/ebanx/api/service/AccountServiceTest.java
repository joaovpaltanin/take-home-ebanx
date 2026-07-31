package com.ebanx.api.service;

import com.ebanx.api.exception.InsufficientFundsException;
import com.ebanx.api.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AccountService")
class AccountServiceTest {

    private AccountService service;

    @BeforeEach
    void setUp() {
        service = new AccountService();
    }

    @Nested
    @DisplayName("depósito")
    class Deposit {

        @Test
        @DisplayName("dado uma conta que não existe, quando depositar, então ela é criada com o valor")
        void givenAnAccountThatDoesNotExist_whenDepositing_thenItIsCreatedWithTheAmount() {
            assertEquals(10, service.deposit("100", 10));
            assertTrue(service.exists("100"));
        }

        @ParameterizedTest
        @DisplayName("dado uma conta com saldo, quando depositar novamente, então o saldo é acumulado")
        @CsvSource({
                "10, 20, 30",
                "1, 1, 2",
                "5, 0, 5",
                "7, 993, 1000"})
        void givenAnAccountWithBalance_whenDepositingAgain_thenTheBalanceIsAccumulated(int initial, int added, int expected) {
            service.deposit("100", initial);

            assertEquals(expected, service.deposit("100", added));
            assertEquals(expected, service.getBalance("100"));
        }
    }

    @Nested
    @DisplayName("saque")
    class Withdraw {

        @ParameterizedTest
        @DisplayName("dado saldo suficiente, quando sacar, então o saldo restante é o esperado")
        @CsvSource({
                "10, 1, 9",
                "10, 9, 1",
                "10, 10, 0",
                "1, 1, 0"})
        void givenSufficientBalance_whenWithdrawing_thenTheRemainingBalanceIsCorrect(int balance, int amount, int remaining) {
            service.deposit("100", balance);

            assertEquals(remaining, service.withdraw("100", amount));
            assertEquals(remaining, service.getBalance("100"));
        }

        @Test
        @DisplayName("dado uma conta que não existe, quando sacar, então falha com not found")
        void givenAnAccountThatDoesNotExist_whenWithdrawing_thenItFailsWithNotFound() {
            assertThrows(NotFoundException.class, () -> service.withdraw("999", 5));
        }

        @Test
        @DisplayName("dado uma conta que não existe, quando sacar, então a conta não é criada")
        void givenAnAccountThatDoesNotExist_whenWithdrawing_thenTheAccountIsNotCreated() {
            assertThrows(NotFoundException.class, () -> service.withdraw("999", 5));

            assertFalse(service.exists("999"));
        }
    }

    @Nested
    @DisplayName("consulta de saldo")
    class GetBalance {

        @Test
        @DisplayName("dado uma conta que não existe, quando consultar o saldo, então falha com not found")
        void givenAnAccountThatDoesNotExist_whenQueryingTheBalance_thenItFailsWithNotFound() {
            assertThrows(NotFoundException.class, () -> service.getBalance("999"));
        }

        @Test
        @DisplayName("dado uma conta zerada, quando consultar o saldo, então retorna zero e a conta continua existindo")
        void givenAnAccountDrainedToZero_whenQueryingTheBalance_thenItReturnsZeroAndKeepsTheAccount() {
            service.deposit("100", 10);
            service.withdraw("100", 10);

            assertEquals(0, service.getBalance("100"));
            assertTrue(service.exists("100"));
        }
    }

    @Nested
    @DisplayName("reset")
    class Reset {

        @Test
        @DisplayName("dado várias contas com saldo, quando resetar, então todas as contas são apagadas")
        void givenSeveralAccountsWithBalance_whenResetting_thenEveryAccountIsWiped() {
            service.deposit("100", 10);
            service.deposit("200", 20);

            service.reset();

            assertThrows(NotFoundException.class, () -> service.getBalance("100"));
            assertThrows(NotFoundException.class, () -> service.getBalance("200"));
        }

        @Test
        @DisplayName("dado um estado vazio, quando resetar duas vezes, então nada falha")
        void givenAnEmptyState_whenResettingTwice_thenNothingFails() {
            service.reset();
            service.reset();

            assertFalse(service.exists("100"));
        }
    }

    @Nested
    @DisplayName("fundos insuficientes")
    class InsufficientFunds {

        @ParameterizedTest
        @DisplayName("dado um saque maior que o saldo, quando sacar, então a operação é rejeitada")
        @ValueSource(ints = {11, 50, 1000})
        void givenAnAmountGreaterThanTheBalance_whenWithdrawing_thenTheOperationIsRejected(int amount) {
            service.deposit("100", 10);

            assertThrows(InsufficientFundsException.class, () -> service.withdraw("100", amount));
        }

        @Test
        @DisplayName("dado um saque rejeitado, quando consultar o saldo, então ele permanece inalterado")
        void givenARejectedWithdrawal_whenQueryingTheBalance_thenItRemainsUnchanged() {
            service.deposit("100", 10);

            attempt(() -> service.withdraw("100", 50));

            assertEquals(10, service.getBalance("100"), "o saldo nunca pode ficar negativo");
        }
    }

    @Nested
    @DisplayName("atomicidade da transferência")
    class TransferAtomicity {

        @Test
        @DisplayName("dado uma origem com fundos insuficientes, quando transferir, então nenhum dinheiro é movimentado")
        void givenAnOriginWithInsufficientFunds_whenTransferring_thenNoMoneyMovesAtAll() {
            service.deposit("100", 10);
            service.deposit("300", 0);

            attempt(() -> service.transfer("100", "300", 50));

            assertEquals(10, service.getBalance("100"), "a origem deve manter seu saldo");
            assertEquals(0, service.getBalance("300"), "o destino não pode ser creditado");
        }

        @ParameterizedTest
        @DisplayName("dado um destino inválido, quando transferir, então a origem mantém seu dinheiro")
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        void givenAnInvalidDestination_whenTransferring_thenTheOriginKeepsItsMoney(String destination) {
            service.deposit("100", 10);

            attempt(() -> service.transfer("100", destination, 5));

            assertEquals(10, service.getBalance("100"), "o dinheiro não pode desaparecer da origem");
        }
    }

    /**
     * Executa uma operação que deverá falhar, para que o teste possa ser afirmado no
     * estado resultante em vez da própria exceção.
     */
    private static void attempt(Runnable operation) {
        try {
            operation.run();
        } catch (RuntimeException ignored) {
        }
    }
}
