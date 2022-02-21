# autocoin-arbitrage-monitor
Part of https://autocoin-trader.com infrastructure.

Service responsibility:
- Monitors currency pairs spreads between various exchanges
- Calculates spread statistics and exposes them for authorized users


To run locally - use RunLocal.kt

# Backlog
Refresh exchange metadata periodically. That should be an async operation
Check reason of frequent log entry with various currency pairs, e.g. ERROR a.p.a.e.a.o.TwoLegOrderBookArbitrageProfitCalculator - First time could not calculate two leg arbitrage profit for CurrencyPairWithExchangePair(currencyPair=TRX/USDT, exchangePair=ExchangePair(firstExchange=BIBOX, secondExchange=BITTREX))
Add blockchain type to metadata

## What are users asking for?
[16.02.2022 22:39]
What I will like to see is the arbitrage on same exchange, without having to move coins between exchanges.

[19.02.2022 09:59]
Hello good morning please I need triangular arbitrage on the same exchange and arbitrage opportunity on another exchange that has high profits

[8.02.2022 01:52]
I'm also seeing opportunities that  stil shows up but the wallet is disable on HitBTC

[6.02.2022 00:16]
By the way, I have a bit of experience  of an arbitrage, within Solana Blockchain.
However, is it possible to monitor the defi exchanges, such as raydium, serum, orca, or a Jupiter liquidity aggregator?
Because the best spreads I get, are between the spot market and defi exchanges

[23.01.2022 17:04]
Hi, decentralized exchanges would be really good, as then you could utilize flash loans from Aave
Any chance to have added Sushiswap, Aave V2, Uniswap V2, Curve, Yearn, Compound ?

Crypto maniac, [22.01.2022 13:20]
I would like to see uniswap(v1 V2 v3) , 1inch and sushiswap

[23.01.2022 17:26]
Would love if you look into adding Loopring and their exchange!

## Done
[26.01.2022 22:01]
Not sure if I'm reading this correct but I notice that DIA/usdt is saying 1.53% I selected market depth of 100,500 and 1000 but when I go to kucoin to buy etc. The withdraw fee is about 20$. U may only break even for an amount bought at 1283$. If u buy in the amount of $100 or $500 u will loose but the monitor  shows 1.5% profit

