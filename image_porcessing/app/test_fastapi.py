import asyncio
from fastapi import FastAPI, Form, UploadFile, File
from fastapi.testclient import TestClient

app = FastAPI()

@app.post("/test")
async def test_route(metadata: str = Form(...), file: UploadFile = File(...)):
    return {"metadata": metadata}

client = TestClient(app)
def run_test():
    import json
    metadata_json = json.dumps({"redBand":1, "nirBand":2})
    files = {
        'file': ('test.tif', b'dummy content', 'application/octet-stream'),
        'metadata': (None, metadata_json, 'application/json')
    }
    response = client.post("/test", files=files)
    print("Test with metadata as tuple:", response.status_code, response.text)

    files2 = {
        'file': ('test.tif', b'dummy content', 'application/octet-stream')
    }
    data = {"metadata": metadata_json}
    response2 = client.post("/test", files=files2, data=data)
    print("Test with metadata as data:", response2.status_code, response2.text)

run_test()
