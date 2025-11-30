import ee
import os
import logging
from dotenv import load_dotenv

load_dotenv()

logger = logging.getLogger(__name__)

class Config:
    # Google Earth Engine credentials
    GEE_SERVICE_ACCOUNT = os.getenv("GEE_SERVICE_ACCOUNT")
    GEE_KEY_FILE = os.getenv("GEE_KEY_PATH")
    # Default image collection
    DEFAULT_COLLECTION = os.getenv("DEFAULT_COLLECTION")  # Updated collection

    # Default image processing parameters
    DEFAULT_PROCESSING_PARAMS = {
        'calculate_ndvi': True,
        'max_cloud_cover': 20,
        'image_scale': 10
    }

def initialize_ee():
    """Initialize Google Earth Engine with service account credentials."""
    try:
        logger.info(f"GEE_SERVICE_ACCOUNT: {Config.GEE_SERVICE_ACCOUNT}")
        logger.info(f"GEE_KEY_FILE: {Config.GEE_KEY_FILE}")
        
        # Verify the key file exists
        if not os.path.exists(Config.GEE_KEY_FILE):
            raise FileNotFoundError(f"Key file not found: {Config.GEE_KEY_FILE}")
        
        # Try to load and validate the JSON
        import json
        with open(Config.GEE_KEY_FILE, 'r') as f:
            key_data = json.load(f)
            logger.info(f"Key file loaded. Service account in file: {key_data.get('client_email')}")
            
            # Check if the email matches
            if key_data.get('client_email') != Config.GEE_SERVICE_ACCOUNT:
                logger.warning(f"⚠️ Service account email mismatch!")
                logger.warning(f"   .env file: {Config.GEE_SERVICE_ACCOUNT}")
                logger.warning(f"   JSON file: {key_data.get('client_email')}")
        
        credentials = ee.ServiceAccountCredentials(
            email=Config.GEE_SERVICE_ACCOUNT,
            key_file=Config.GEE_KEY_FILE
        )
        logger.info("Credentials set. Initializing EE...")
        ee.Initialize(credentials)
        logger.info("✅ GEE initialized successfully")
    except Exception as e:
        logger.error(f"❌ GEE initialization failed: {str(e)}")
        logger.error("💡 Possible solutions:")
        logger.error("   1. Download a fresh service account key from Google Cloud Console")
        logger.error("   2. Verify the service account email matches the key file")
        logger.error("   3. Ensure the service account has Earth Engine permissions")
        raise
