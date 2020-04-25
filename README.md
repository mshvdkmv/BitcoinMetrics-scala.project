# BitcoinMetrics-scala.project

### Project description:
This is a scala-based application, that performs metrics calculation, which is used in the Bitcoin price prediction task.
The results of computations could be accessed in two ways, which provide flexibility in use:
- Get requests to the server
- Local file access

These metrics could be divided into two groups: exchanges metrics and blockchain network metrics.

#### Exchanges metrics:
The following set of metrics calculated is calculated for the XBTUSD asset.
The source of information is a specified set of exchanges, that could be defined in reference.conf file.
- Asks VWAP
- Bids VWAP
- VWAP midpoint
- Meeting point

#### Blockchain metrics:
These metrics are calculated for the predefined window number of last blocks, which could be defined in reference.conf file.
For each of the metrics from the following list the average, max, and min values are calculated.
- Total fee in a block
- Crypto value in a block
- Number of transactions in a block
- Number of unsigned transactions at the Blockchain network.

## How to run:
To run the project, you need to download the repo, build the project, specify the directory for the result storage.

## How to access the results:

After the run of the application, the metrics could be accessed from the servers:

http://localhost:8080/exchanges-metrics - for the exchanges metrics

http://localhost:8080/blockchain-metrics - for the blokchain metrics

OR 

by accessing ```exchanges_metrics.txt``` and ```blockchain_metrics.txt``` files stored in a specified directory.

