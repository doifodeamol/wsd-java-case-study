# Solvians Java Case Study

## Introduction – Feed Generator

Our core business is to create financial websites for some of the top-tier financial institutions in the world. Those pages display information about financial products or so-called “certificates”, sourcing from numerous data feeds in our back-end system.

Certificate updates of data feed are the most important element in our applications. A certificate update has the following properties:

* Timestamp (number of milliseconds since January 1, 1970, 00:00:00 GMT)
* ISIN (string, 2 random uppercase alphabets + 9 random alphanumeric characters + 1 check digit)
* Bid Price (random number, 2 decimal places, range between 100.00 and 200.00 inclusive)
* Bid Size (random number, 0 decimal place, range between 1,000 and 5,000 inclusive)
* Ask Price (random number, 2 decimal places, range between 100.00 and 200.00 inclusive)
* Ask Size (random number, 0 decimal place, range between 1,000 and 10,000 inclusive)
* ISIN check digit calculation: 

To calculate the check digit, the following steps should be followed:
1.	Convert any letters to numbers by the conversion table below, e.g. “DE123456789” will be converted to “13 14 1 2 3 4 5 6 7 8 9”
2.	Starting from the rightmost digit, every other digit is multiplied by two. In the example, we will have “2 3 2 4 2 2 6 4 10 6 14 8 18”.
3.	Add up the resulting string of digits (numbers greater than 9 becoming two separate digits). In the example, we will have 2+3+2+4+2+2+6+4+1+0+6+1+4+8+1+8 = 54.
4.	Subtract the sum from the smallest multiple of 10 which is greater than or equal to it, in the example we will have 60 – 54 = 6. 

Conversion table for characters is:

```
A = 10	F = 15	K = 20	P = 25	U = 30	Z = 35
B = 11	G = 16	L = 21	Q = 26	V = 31	
C = 12	H = 17	M = 22	R = 27	W = 32	
D = 13	I = 18	N = 23	S = 28	X = 33	
E = 14	J = 19	O = 24	T = 29	Y = 34	
```
## Implementation – Feed Generator

* Write an ISIN Generator class with Unit tests to generate the ISIN string. This is the most central part of the case study.

* Write a Callable<String> class with Unit test to generate one line of certificate update, in which the described properties are comma separated (thousand-separator is not needed):
    `1352122280502,DE1234567896,101.23,1000,103.45,1000`

* Generating random numbers is not important in this implementation, you can write a very simple generator or use the following sample: 
```java
        ThreadLocalRandom random = ThreadLocalRandom.current();
```

* Use the `App` main class to
  - Take 2 parameters: (a) Number of threads, and (b) Number of certificate updates, (this is already implemented and tested, but feel free to improve it and fix any issues)
  - According to the parameters, trigger the certificate generations in multiple threads, and
  - Finally, collect and print the lines of generated certificate updates

We will run our own tests on your code so bear that in mind when designing classes


---

## Solution notes

* `IsinGenerator` – generates ISINs, `calculateCheckDigit(...)` and `isValid(...)` are public static helpers.
* `CertificateUpdateTask` – the `Callable<String>` producing one feed line.
* `CertificateUpdateGenerator` – runs the tasks on a fixed thread pool (`threads * quotes` updates in total).
* `App` – `java -cp target/classes com.solvians.showcase.App <threads> <quotes>`.

See `EXPLAINED.md` for a plain-language walkthrough.
