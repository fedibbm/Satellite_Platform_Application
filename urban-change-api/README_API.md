\# API Reference



\## Base URL



https://achref192-urban-change-api.hf.space



\## Endpoints



\### GET /



Returns basic information about the service.



\### GET /health



Returns API health status and indicates whether the model is loaded.



\### POST /predict-change



Runs change detection on two uploaded images.



\## Request format for POST /predict-change



Send a multipart/form-data request with:



\- `image\_a`: image before

\- `image\_b`: image after



\## Response format



Example response:



```json

{

&#x20; "request\_id": "example-id",

&#x20; "message": "Inference completed",

&#x20; "summary": {

&#x20;   "patch\_id": "example-id",

&#x20;   "status": "ok",

&#x20;   "height": 1024,

&#x20;   "width": 1024,

&#x20;   "pred\_ratio\_final": 0.12,

&#x20;   "prob\_mean": 0.08,

&#x20;   "global\_prob\_mean": 0.07

&#x20; },

&#x20; "files": {

&#x20;   "overlay\_pred": "https://.../result/example-id/overlay",

&#x20;   "mask\_final": "https://.../result/example-id/mask",

&#x20;   "panel": "https://.../result/example-id/panel",

&#x20;   "prob": "https://.../result/example-id/prob",

&#x20;   "outputs\_npz": "https://.../result/example-id/npz"

&#x20; }

}

