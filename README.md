<p align="center">
  <img src="docs/icone.png" width="112" alt="Ícone do app Corrida Particular">
</p>

<h1 align="center">Corrida Particular</h1>

<p align="center">
  App Android gratuito e de código aberto para <b>motoristas particulares</b>:<br>
  taxímetro por GPS, orçamento com previsão de trânsito, agenda de corridas, clientes e recibos pelo WhatsApp.
</p>

<p align="center">
  <a href="https://github.com/robertobegood-jpg/Corrida-Particular-app-Android/releases/latest"><b>⬇️ Baixar o APK</b></a> ·
  <a href="#-cafezinho-para-o-dev"><b>☕ Apoiar o projeto</b></a> ·
  <a href="https://github.com/robertobegood-jpg/Corrida-Particular-app-Android/issues"><b>💬 Sugestões e problemas</b></a>
</p>

---

## Funções

**Menu inferior:** Taxímetro • Orçamento • Agenda • Clientes • Mais

### 🚕 Taxímetro
- Conta pelo GPS: bandeirada + km rodado + hora parada (abaixo de 10 km/h).
- Bandeira 1 e 2 (horário noturno e domingo configuráveis), pausar e retomar.
- **Aguardando passageiro:** tempo grátis no embarque ou numa parada e, depois, cobrança por minuto somada automaticamente.
- Continua rodando ao trocar de aba, com a tela bloqueada ou em outro app. O valor fica visível na notificação.
- Ao finalizar, salva no histórico e envia o recibo pelo WhatsApp (texto ou PDF).

### 🧮 Orçamento
- Cliente cadastrado (origem já preenchida) ou corrida avulsa.
- Paradas, destino e horário ("agora" ou data e hora futura).
- Tempo de viagem com **previsão de trânsito** da TomTom para o horário escolhido.
- Envio por WhatsApp em texto ou PDF, agendamento ou início direto do taxímetro.
- Modo manual (km e minutos) para quando não houver internet.

### 🗓️ Agenda
- Corridas agendadas por dia, com lembrete antes do horário.
- Iniciar o taxímetro, concluir, editar, enviar confirmação ou cancelar.
- Histórico com ganhos do dia e do mês, recibos e "repetir corrida".

### 👥 Clientes
- Nome, WhatsApp, observações, endereços de embarque (Casa, Trabalho…) e destinos frequentes.
- **Importação dos contatos do celular** (nome, telefone e endereços).

### 🧭 Navegação
- Abre a rota no **Google Maps** com todas as paradas, ou leva até cada ponto pelo **Waze**, Maps ou outro app de GPS.

### ⚙️ Mais
- Tarifas, dados do motorista (saem no orçamento e no recibo), chave TomTom, backup e restauração.

---

## Instalação

1. Baixe o arquivo `.apk` da [última versão](https://github.com/robertobegood-jpg/Corrida-Particular-app-Android/releases/latest) pelo celular.
2. Abra o arquivo e permita **"instalar apps desconhecidos"** quando o Android pedir.
3. Para atualizar, baixe a versão nova e instale por cima: os dados continuam salvos.

> Requer Android 8.0 ou superior.

### Primeiros passos
1. **Mais › Tarifas:** ajuste os valores (os de fábrica são só exemplos).
2. **Mais › Configurações:** preencha seus dados e cole sua chave gratuita da [TomTom](https://developer.tomtom.com/) (usada para buscar endereços e calcular rotas com trânsito).
3. Na primeira corrida, permita **Localização** e **Notificações**.
4. Para o taxímetro não parar em segundo plano: *Configurações do Android › Apps › Corrida Particular › Bateria → Sem restrições*. Nos Xiaomi, ative também a *Inicialização automática*.

---

## Privacidade

- **Não há servidor nem cadastro.** Clientes, corridas e configurações ficam só no seu celular.
- A localização é usada apenas pelo taxímetro, durante a corrida.
- Os endereços digitados são enviados à TomTom, com a **sua** chave, só para busca e cálculo de rota.
- O backup é um arquivo `.json` que você salva ou envia para onde quiser.

### Permissões
| Permissão | Para quê |
|---|---|
| Localização | Medir a distância no taxímetro e usar "minha localização" nos endereços |
| Notificações | Mostrar o taxímetro em andamento e os lembretes da agenda |
| Contatos | Importar clientes (opcional) |
| Internet | Busca de endereços e rotas na TomTom |

---

## ☕ Cafezinho para o Dev

O app é gratuito. Se ele ajuda no seu trabalho, você pode apoiar o desenvolvimento com um Pix de qualquer valor. Também dá para fazer isso direto no app, em **Mais › Pagar um cafezinho ao Dev**, com QR Code.

- **Chave Pix (e-mail):** `robertobegood+itau@gmail.com`
- **Nome:** Roberto Santos da Silva

**Pix copia e cola** (valor livre):
```
00020126500014br.gov.bcb.pix0128robertobegood+itau@gmail.com5204000053039865802BR5923ROBERTO SANTOS DA SILVA6009SAO PAULO62070503***63040CE3
```

Obrigado! 💛

---

## Para desenvolvedores

Kotlin + Jetpack Compose, usando só as bibliotecas do Android e o [ZXing](https://github.com/zxing/zxing) (QR Code).

```
app/src/main/java/com/corridaparticular/app/
  MainActivity.kt          menu inferior e navegação
  data/                    modelos, armazenamento local (Repo) e cálculo das tarifas
  net/                     TomTom (busca e rotas com trânsito) e localização
  taximetro/               serviço do taxímetro em segundo plano (GPS)
  share/                   mensagens de WhatsApp e geração de PDF
  agenda/                  lembretes das corridas agendadas
  util/                    Pix, contatos, navegação e formatação
  ui/                      telas
```

**Compilar:** abra a pasta no Android Studio e use *Build › Build APK(s)*, ou rode `./gradlew assembleDebug`. Sem a chave oficial, o APK sai assinado com a chave de debug padrão — funciona normalmente, mas não atualiza por cima da versão oficial.

**Publicar uma versão** (mantenedor): suba `versionCode` e `versionName` em `app/build.gradle.kts`, faça o commit e crie uma tag `vX.Y`. O GitHub Actions compila, assina e publica a Release com o APK.

Sugestões e correções são bem-vindas por [issues](https://github.com/robertobegood-jpg/Corrida-Particular-app-Android/issues) e pull requests.

## Licença

[GPL-3.0](LICENSE) — você pode usar, estudar, modificar e redistribuir, desde que as versões derivadas também sejam abertas sob a mesma licença.

Desenvolvido por Roberto Santos da Silva.
