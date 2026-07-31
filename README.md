# Take Home EBANX API

API HTTP simples que simula operações bancárias em memória: consulta de saldo, depósito, saque e transferência. Construída em Java puro, usando apenas a biblioteca padrão (`com.sun.net.httpserver.HttpServer`), sem frameworks web externos.

## Requisitos

- Java 21+ (o projeto usa virtual threads via `Executors.newVirtualThreadPerTaskExecutor()`)
- Maven 3.9+ (para compilar, testar e empacotar)

## Estrutura do projeto

```
.
├── pom.xml
└── src
    ├── main
    │   └── java
    │       └── com
    │           └── ebanx
    │               └── api
    │                   ├── Main.java                     # Ponto de entrada, sobe o servidor na porta 8080
    │                   ├── handler
    │                   │   ├── BaseHandler.java          # Métodos utilitários (parsing de query/JSON, envio de resposta)
    │                   │   ├── BalanceHandler.java     # GET /balance
    │                   │   ├── EventHandler.java         # POST /event
    │                   │   └── ResetHandler.java         # POST /reset
    │                   ├── service
    │                   │   ├── AccountService.java        # Regras de negócio e armazenamento em memória
    │                   │   └── TransferResult.java       # Record com saldos após uma transferência
    │                   └── exception
    │                       ├── BadRequestException.java
    │                       ├── InsufficientFundsException.java
    │                       └── NotFoundException.java
    └── test
        └── java
            └── com
                └── ebanx
                    └── api
                        └── service
                            └── AccountServiceTest.java  # Testes unitários com JUnit 5
```

## Como rodar

```bash
# Compilar e rodar os testes
mvn test

# Empacotar em um JAR executável
mvn package

# Executar o JAR gerado
java -jar target/take-home-ebanx-1.0.0.jar
```

O servidor sobe em `http://localhost:8080`.

> Também é possível compilar manualmente sem o Maven, desde que o Java 21+ esteja instalado:
> ```bash
> javac -d out $(find src/main/java -name "*.java")
> java -cp out com.ebanx.api.Main
> ```

## Endpoints

### `GET /balance?account_id={id}`

Retorna o saldo de uma conta.

| Status | Condição |
|--------|----------|
| 200 | Retorna o saldo (corpo é só o número) |
| 400 | `account_id` ausente ou não é um inteiro positivo |
| 404 | Conta não encontrada |

**Exemplo:**
```
GET /balance?account_id=100
200 -> 10
```

### `POST /reset`

Limpa todas as contas em memória. Não recebe corpo.

```
POST /reset
200 -> OK
```

### `POST /event`

Executa uma operação bancária. O corpo é um JSON com o campo `type`, que pode ser `deposit`, `withdraw` ou `transfer`.

Respostas de erro comuns:

| Status | Condição |
|--------|----------|
| 400 | `type` desconhecido, `amount` ausente, zero ou negativo, ou IDs inválidos |
| 404 | Conta informada não existe |

#### `deposit`

Cria a conta de destino caso ela não exista.

```json
// Request
{ "type": "deposit", "destination": "100", "amount": 10 }

// Response 201
{ "destination": { "id": "100", "balance": 10 } }
```

#### `withdraw`

Saca de uma conta existente. Rejeita a operação se o saldo for insuficiente.

```json
// Request
{ "type": "withdraw", "origin": "100", "amount": 5 }

// Response 201
{ "origin": { "id": "100", "balance": 5 } }
```

| Status | Condição |
|--------|----------|
| 404 | Conta de origem não existe |
| 500 | Saldo insuficiente (a exceção `InsufficientFundsException` não é mapeada no handler atual) |

#### `transfer`

Transfere valores entre duas contas existentes. A operação é atômica (sincronizada) e, em caso de erro, nenhum valor é movimentado.

```json
// Request
{ "type": "transfer", "origin": "100", "destination": "300", "amount": 15 }

// Response 201
{
  "origin": { "id": "100", "balance": 0 },
  "destination": { "id": "300", "balance": 15 }
}
```

| Status | Condição |
|--------|----------|
| 400 | `origin` ou `destination` ausentes, vazios ou inválidos |
| 404 | Conta de origem não existe |
| 500 | Saldo insuficiente na origem |

## Detalhes técnicos

- **Armazenamento**: em memória, via `ConcurrentHashMap`. Os dados são perdidos ao reiniciar o servidor ou ao chamar `/reset`.
- **Parsing de JSON**: feito com regex simples (`extractString` / `extractInt`), sem biblioteca externa. Funciona para o formato plano usado pela API, mas não é um parser JSON genérico.
- **Concorrência**: cada requisição é tratada em uma virtual thread.
- **Atomicidade**: transferências são sincronizadas para evitar estados inconsistentes quando ocorrem erros (ex.: fundos insuficientes ou destino inválido).
- **Testes**: cobrem depósito, saque, consulta de saldo, reset, fundos insuficientes e atomicidade da transferência.
