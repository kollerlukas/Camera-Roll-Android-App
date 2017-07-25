package us.koller.cameraroll.util;

/*simple class to handle Rational numbers from Exif data easier*/
/*own class, since android.util.Rational only for api 21 and up*/
class Rational {

    private int numerator, denominator;

    static Rational parseRational(String input) {
        String[] parts = input.split("/");
        int numerator = Integer.valueOf(parts[0]);
        int denominator = Integer.valueOf(parts[1]);
        return new Rational(numerator, denominator);
    }

    @SuppressWarnings("WeakerAccess")
    public Rational(int numerator, int denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    float floatValue() {
        return (float) numerator / (float) denominator;
    }

    void setDenominator(int denominator) {
        this.denominator = denominator;
    }

    int getDenominator() {
        return denominator;
    }
}
