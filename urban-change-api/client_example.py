import requests

URL = "http://localhost:8020/predict-change"

with open("A.png", "rb") as fa, open("B.png", "rb") as fb:
    response = requests.post(
        URL,
        files={
            "image_a": ("A.png", fa, "image/png"),
            "image_b": ("B.png", fb, "image/png"),
        },
        timeout=600,
    )

print("Status:", response.status_code)
print(response.json())