package Domus;

import net.automatalib.words.impl.GrowingMapAlphabet;

// new class that overrides the getSymbolIndex method so that if in testing it finds a new symbol,
// it simply adds it to the alphabet without throwing exceptions
public class DomusAlphabet<I> extends GrowingMapAlphabet<I> {
    @Override
    public int getSymbolIndex(I symbol) {
        Integer result = indexMap.get(symbol);
        if (result == null) {
            super.add(symbol);
        }
        result = indexMap.get(symbol);
        return result;
    }
}
