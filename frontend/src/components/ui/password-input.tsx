"use client";
import { useState, type ComponentProps } from "react";
import { Eye, EyeOff } from "lucide-react";
import { Input } from "./input";
export function PasswordInput(props: ComponentProps<"input">) {
  const [senhaVisivel, definirSenhaVisivel] = useState(false);
  return (
    <div className="relative">
      <Input
        {...props}
        type={senhaVisivel ? "text" : "password"}
        className="pr-14"
      />
      <button
        type="button"
        aria-label={senhaVisivel ? "Ocultar senha" : "Mostrar senha"}
        aria-pressed={senhaVisivel}
        disabled={props.disabled}
        className="absolute top-1 right-1 flex size-10 items-center justify-center rounded-lg text-muted-foreground hover:bg-muted"
        onClick={() => definirSenhaVisivel(!senhaVisivel)}
      >
        {senhaVisivel ? (
          <EyeOff size={18} aria-hidden />
        ) : (
          <Eye size={18} aria-hidden />
        )}
      </button>
    </div>
  );
}
