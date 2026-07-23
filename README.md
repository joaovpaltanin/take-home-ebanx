# Take Home EBANX API

API HTTP simples que simula operações bancárias em memória: consulta de saldo, depósito, saque e transferência. Construída em Java puro, usando apenas a biblioteca padrão (`com.sun.net.httpserver.HttpServer`), sem frameworks externos.

## Requisitos

- Java 21+ (o projeto usa virtual threads via `Executors.newVirtualThreadPerTaskExecutor()`)

## Estrutura do projeto

```
com.ebanx.api
├── Main.java                     # Ponto de entrada, sobe o servidor na porta 8080
├── handler
│   ├── BaseHandler.java           # Métodos utilitários (parsing de query/JSON, envio de resposta)
│   ├── BalanceHandler.java        # GET /balance
│   ├── EventHandler.java          # POST /event
│   └── ResetHandler.java          # POST /reset
├── service
│   └── AccountService.java        # Regras de negócio e armazenamento em memória
└── exception
    ├── BadRequestException.java
    └── NotFoundException.java
```

## Como rodar

```bash
# Compilar
javac -d out $(find src -name "*.java")

# Executar
java -cp out com.ebanx.api.Main
```

O servidor sobe em `http://localhost:8080`.

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

#### `deposit`

```json
// Request
{ "type": "deposit", "destination": "100", "amount": 10 }

// Response 201
{ "destination": { "id": "100", "balance": 10 } }
```

#### `withdraw`

```json
// Request
{ "type": "withdraw", "origin": "100", "amount": 5 }

// Response 201
{ "origin": { "id": "100", "balance": 5 } }
```
Retorna **404** se a conta de origem não existir.

#### `transfer`

```json
// Request
{ "type": "transfer", "origin": "100", "destination": "300", "amount": 15 }

// Response 201
{
  "origin": { "id": "100", "balance": 0 },
  "destination": { "id": "300", "balance": 15 }
}
```
Retorna **404** se a conta de origem não existir.

Em qualquer operação, um `amount` inválido (ausente ou não positivo) ou um `type` desconhecido retorna **400**.

## Detalhes técnicos

- **Armazenamento**: em memória, via `ConcurrentHashMap`. Os dados são perdidos ao reiniciar o servidor ou ao chamar `/reset`.
- **Parsing de JSON**: feito com regex simples (`extractString` / `extractInt`), sem biblioteca externa. Funciona para o formato plano usado pela API, mas não é um parser JSON genérico.
- **Concorrência**: cada requisição é tratada em uma virtual thread.