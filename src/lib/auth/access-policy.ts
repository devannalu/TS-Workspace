export function hasActiveProfile(profile: { status: string } | null | undefined): boolean {
  return profile?.status === "active";
}
