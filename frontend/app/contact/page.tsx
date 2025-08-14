import { FiMail, FiLinkedin } from 'react-icons/fi';

export default function ContactPage() {
  return (
    <main className="p-6 flex flex-col items-center">
      <h1 className="text-3xl font-bold mb-8">Contact</h1>
      <section className="flex items-center justify-center gap-4 mb-6">
        <a href="mailto:bali.redouane@gmail.com" className="text-xl text-[#EA4335] hover:text-[#C53929]">
          <FiMail size={48} />
        </a>
      </section>
      <section className="flex items-center justify-center gap-4">
        <a
          href="https://www.linkedin.com/in/redouane-bali/"
          target="_blank"
          rel="noopener noreferrer"
          className="text-xl text-[#0A66C2] hover:text-[#084B8A]"
        >
          <FiLinkedin size={48} />
        </a>
      </section>
    </main>
  );
}
