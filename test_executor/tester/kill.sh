#!/bin/bash

kill -9 $(lsof -ti tcp:8000)
