# Makefile

CC = gcc
CFLAGS = -Wall -Wextra -O2 -std=c99
TARGET = test_large
OBJS = t27.o test_large.o

all: $(TARGET)

$(TARGET): $(OBJS)
	$(CC) $(CFLAGS) -o $(TARGET) $(OBJS)

t27.o: t27.c t27.h
	$(CC) $(CFLAGS) -c t27.c

test_large.o: test_large.c t27.h
	$(CC) $(CFLAGS) -c test_large.c

clean:
	rm -f $(TARGET) $(OBJS)
