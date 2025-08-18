import React, { useState, useEffect, useRef } from 'react';
import Image from 'next/image';

const images = [
  '/screen1.png',
  '/screen2.png',
  '/screen3.png',
  '/screen4.png',
];

const AUTO_PLAY_INTERVAL = 4000;

const ImageSlider: React.FC = () => {
  const [currentIndex, setCurrentIndex] = useState(0);
  const timeoutRef = useRef<NodeJS.Timeout | null>(null);

  const resetTimeout = () => {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
    }
  };

  useEffect(() => {
    resetTimeout();
    timeoutRef.current = setTimeout(() => {
      setCurrentIndex((prevIndex) =>
        prevIndex === images.length - 1 ? 0 : prevIndex + 1
      );
    }, AUTO_PLAY_INTERVAL);

    return () => {
      resetTimeout();
    };
  }, [currentIndex]);

  return (
    <div className="relative w-full max-w-4xl mx-auto overflow-hidden rounded-lg">
      <div
        className="flex transition-transform duration-700 ease-in-out"
        style={{ transform: `translateX(-${currentIndex * 100}%)` }}
      >
        {images.map((src, index) => (
          <div key={index} className="flex-shrink-0 w-full flex justify-center relative h-160 sm:h-96">
            <Image
              src={src}
              alt={`Slide ${index + 1}`}
              width={300}
              height={600}
              style={{ objectFit: 'contain' }}
              priority={index === 0}
            />
          </div>
        ))}
      </div>
      <div className="flex justify-center mt-4 space-x-3">
        {images.map((_, idx) => (
          <button
            key={idx}
            className={`w-3 h-3 rounded-full transition-colors ${
              idx === currentIndex ? 'bg-blue-600' : 'bg-gray-300'
            }`}
            onClick={() => setCurrentIndex(idx)}
            aria-label={`Go to slide ${idx + 1}`}
          />
        ))}
      </div>
    </div>
  );
};

export default ImageSlider;