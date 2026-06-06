"""Tests for the routing engine logic (no DB required)."""
import pytest

from app.services.routing_engine import CATEGORY_ROUTING, detect_category


def test_detect_category_image():
    assert detect_category("image/jpeg") == "images"
    assert detect_category("image/png") == "images"
    assert detect_category("image/webp") == "images"


def test_detect_category_video():
    assert detect_category("video/mp4") == "videos"
    assert detect_category("video/x-matroska") == "videos"


def test_detect_category_audio():
    assert detect_category("audio/mpeg") == "audio"
    assert detect_category("audio/wav") == "audio"


def test_detect_category_document():
    assert detect_category("application/pdf") == "documents"
    assert detect_category("application/msword") == "documents"
    assert detect_category("text/plain") == "documents"
    assert detect_category("text/csv") == "documents"


def test_detect_category_archive():
    assert detect_category("application/zip") == "archives"
    assert detect_category("application/x-7z-compressed") == "archives"
    assert detect_category("application/gzip") == "archives"


def test_detect_category_other():
    assert detect_category("application/octet-stream") == "other"
    assert detect_category("") == "other"
    assert detect_category("application/unknown") == "other"


def test_category_routing_map_complete():
    required = {"images", "documents", "videos", "audio", "archives", "other"}
    assert required == set(CATEGORY_ROUTING.keys())


def test_category_routing_values_are_valid_providers():
    valid = {"google", "onedrive", "mega", "box", "pcloud", "dropbox"}
    for cat, prov in CATEGORY_ROUTING.items():
        assert prov in valid, f"{cat} maps to unknown provider '{prov}'"
