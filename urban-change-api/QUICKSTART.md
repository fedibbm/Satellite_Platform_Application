\# Quick Start



\## 1) Open the API documentation



Open this link in your browser:



https://achref192-urban-change-api.hf.space/docs



\## 2) Test from browser



\- Open `POST /predict-change`

\- Click \*\*Try it out\*\*

\- Upload:

&#x20; - `image\_a`

&#x20; - `image\_b`

\- Click \*\*Execute\*\*



\## 3) Expected response



The API returns:



\- `request\_id`

\- `message`

\- `summary`

\- `files.overlay\_pred`

\- `files.mask\_final`

\- `files.panel`

\- `files.prob`

\- `files.outputs\_npz`



\## 4) Open result links



Use the returned URLs to access:



\- final overlay

\- final mask

\- panel visualization

\- probability map

\- NPZ outputs



\## 5) Health check



To verify that the service is online:



https://achref192-urban-change-api.hf.space/health

