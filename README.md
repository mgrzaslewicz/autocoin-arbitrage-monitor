# autocoin-arbitrage-monitor
Part of https://autocoin-trader.com infrastructure.

Service responsibility:
- Monitors currency pairs spreads between various exchanges
- Calculates spread statistics and exposes them for authorized users

To run locally - use RunLocal.kt

# Backlog

Refresh exchange metadata periodically. That should be an async operation
Check reason of frequent log entry with various currency pairs, e.g. ERROR a.p.a.e.a.o.TwoLegOrderBookArbitrageProfitCalculator - First time could not calculate two leg arbitrage
profit for CurrencyPairWithExchangePair(currencyPair=TRX/USDT, exchangePair=ExchangePair(firstExchange=BIBOX, secondExchange=BITTREX))
Add blockchain type to metadata

## What are users asking for?

- B, [04.12.2022]

1. I would like to see split filters for exchanges,

- Buy From exchange
- Sell On exchange

2. I want to see the equivalent of $ for each oportunity and order the results ascending or descending.

3. Transfer status (Withdraw & deposit status), should be transferred to filters. If i don't want to see yellow status i should be able to exclude it.

4. Base currencies and Quote currencies should be splited in filter.(for example i dont own BTC, and i would like to exclude it from the results)

5. If after applying filters there are no results, can the page play a sound when the result appears ? Or can it send Telegram message with short opportunity details ?

6. About automation.. if an arbitrage opportunity arises, can it place orders using API keys ?

7. Cryptopia in API keys ??? its a fuckedup exchange, i lost some money because of their scam :)

8. Can you add more exchanges (ByBit, Huobi) ?

- Ogunleke, [16.05.2022 12:11]
  That triangular arbitrage is the best
- İsmayıl, [16.05.2022 12:01]
  Hi, I have one question about arbitrage, can we make arbitraje in a same exchange? For ex,maning a triangular arbitrage: Buy btc, convert to eth, sell eth to usdt, then repeat
  again. Is this software able to find this kind of opportunities?

- [15.05.2022 19:36]
  can I filter the ERc20 token?

- [16.02.2022 22:39]
What I will like to see is the arbitrage on same exchange, without having to move coins between exchanges.

- [19.02.2022 09:59]
Hello good morning please I need triangular arbitrage on the same exchange and arbitrage opportunity on another exchange that has high profits

- [8.02.2022 01:52]
I'm also seeing opportunities that  stil shows up but the wallet is disable on HitBTC

- [6.02.2022 00:16]
By the way, I have a bit of experience  of an arbitrage, within Solana Blockchain.
However, is it possible to monitor the defi exchanges, such as raydium, serum, orca, or a Jupiter liquidity aggregator?
Because the best spreads I get, are between the spot market and defi exchanges

- [23.01.2022 17:04]
Hi, decentralized exchanges would be really good, as then you could utilize flash loans from Aave
Any chance to have added Sushiswap, Aave V2, Uniswap V2, Curve, Yearn, Compound ?

- Crypto maniac, [22.01.2022 13:20]
I would like to see uniswap(v1 V2 v3) , 1inch and sushiswap

- [23.01.2022 17:26]
Would love if you look into adding Loopring and their exchange!

## Done
[26.01.2022 22:01]
Not sure if I'm reading this correct but I notice that DIA/usdt is saying 1.53% I selected market depth of 100,500 and 1000 but when I go to kucoin to buy etc. The withdraw fee is about 20$. U may only break even for an amount bought at 1283$. If u buy in the amount of $100 or $500 u will loose but the monitor  shows 1.5% profit

