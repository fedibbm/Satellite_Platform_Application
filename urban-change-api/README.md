\# Urban Change Detection API



Public API for urban change detection from bi-temporal image pairs.



This project provides a hosted API that takes:



\- image A (before)

\- image B (after)



and returns:



\- final binary change mask

\- overlay visualization

\- probability map

\- result panel

\- downloadable NPZ outputs



\## Live API



\- Swagger Docs: https://achref192-urban-change-api.hf.space/docs

\- Health Check: https://achref192-urban-change-api.hf.space/health

\- Predict Endpoint: https://achref192-urban-change-api.hf.space/predict-change



\## Main Features



\- Pairwise image change detection

\- Hosted public API

\- Easy testing from browser

\- JSON response with downloadable result files

\- Client example included



\## Quick Start



See \[QUICKSTART.md](QUICKSTART.md)



\## API Reference



See \[README\_API.md](README\_API.md)



\## Python Example



See \[client\_example.py](client\_example.py)



\## Example Workflow



1\. Open the Swagger docs

2\. Upload image A and image B

3\. Execute prediction

4\. Get returned JSON and result links



\## Notes



\- The API is publicly accessible online

\- No local network access is required

\- The service is hosted externally on Hugging Face Spaces

